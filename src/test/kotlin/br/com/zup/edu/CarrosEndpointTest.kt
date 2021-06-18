package br.com.zup.edu


import carros.Carro
import carros.CarroRepository
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub,
    val carrosRepository: CarroRepository
) {

    @BeforeEach
    internal fun setUp() {
        carrosRepository.deleteAll()
    }


    @Test
    fun `Deve adicionar um novo carro`(){

        val response = grpcClient.adicionar(CarroRequest.newBuilder()
            .setModelo("Gol")
            .setPlaca("HPX-1234")
            .build())


        with(response){
            assertNotNull(id)
            assertTrue(carrosRepository.existsById(id))
        }

    }

    @Test
    fun `Nao deve adicionar novo carro quando placa ja for existente`() {

       //cenario
        val existente = carrosRepository.save(Carro("Golzin", "OTZ-0004"))

        //ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("Gol")
                    .setPlaca(existente.placa)
                    .build()
            )
        }

        //validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
        }

    }

    @Test
    fun `Nao deve adicionar novo carro quando dados de entrada forem invalidos`() {

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("EcoSport")
                    .setPlaca("")
                    .build()
            )
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("Dados de entrada inválidos", this.status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}